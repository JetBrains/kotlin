// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.objectTree.ThrowableInterner;
import com.intellij.util.SmartList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class ObjectNode {
    private static final int REASONABLY_BIG = 500;

    private final Disposable myObject;
    private @NotNull NodeChildren myChildren = EMPTY; // guarded by ObjectTree.treeLock
    private Throwable myTrace; // guarded by ObjectTree.treeLock

    private ObjectNode(@NotNull Disposable object, boolean parentIsRoot) {
        myObject = object;
        myTrace = parentIsRoot && Disposer.isDebugMode() ? ThrowableInterner.intern(new Throwable()) : null;
    }

    // root
    private ObjectNode() {
        myObject = ROOT_DISPOSABLE;
    }

    private static final Disposable ROOT_DISPOSABLE = Disposer.newDisposable("ROOT_DISPOSABLE");

    void assertNoChildren(boolean throwError) {
        for (ObjectNode childNode : myChildren.getAllNodes()) {
            Disposable object = childNode.getObject();
            Throwable trace = childNode.getTrace();
            String message =
                    "Memory leak detected: '" + object + "' (" + object.getClass() + ") was registered in Disposer" +
                    " as a child of '"+getObject()+"' (" + getObject().getClass() + ")"+
                    " but wasn't disposed.\n" +
                    "Register it with a proper parentDisposable or ensure that it's always disposed by direct Disposer.dispose call.\n" +
                    "See https://jetbrains.org/intellij/sdk/docs/basics/disposers.html for more details.\n" +
                    "The corresponding Disposer.register() stacktrace is shown as the cause:\n";
            RuntimeException exception = new RuntimeException(message, trace);
            if (throwError) {
                throw exception;
            }
            ObjectTree.getLogger().error(exception);
        }
    }

    private boolean isRootNode() {
        return myObject == ROOT_DISPOSABLE;
    }

    static @NotNull ObjectNode createRootNode() {
        return new ObjectNode();
    }

    private void addChildNode(@NotNull ObjectNode childNode) {
        NodeChildren children = myChildren;
        NodeChildren newChildren = children.addChildNode(childNode);
        if (children != newChildren) {
            myChildren = newChildren;
        }
    }

    void removeChildNode(@NotNull ObjectNode childNode) {
        myChildren.removeChildNode(childNode.getObject());
    }

    @NotNull
    ObjectNode moveChildNodeToOtherParent(@NotNull Disposable child, @NotNull ObjectNode otherParentNode) {
        ObjectNode childNode = myChildren.removeChildNode(child);
        if (childNode == null) {
            childNode = new ObjectNode(child, isRootNode());
        }
        otherParentNode.addChildNode(childNode);
        assert childNode.getObject() == child;
        return childNode;
    }

    /**
     * {@code predicate} is used only for direct children
     */
    void removeChildNodesRecursively(@NotNull List<? super Disposable> disposables,
            @NotNull ObjectTree tree,
            @Nullable Throwable trace,
            @Nullable Predicate<? super Disposable> predicate) {
        myChildren.removeChildren(predicate, childNode -> {
            // predicate is used only for direct children
            childNode.removeChildNodesRecursively(disposables, tree, trace, null);
            // already disposed. may happen when someone does `register(obj, ()->Disposer.dispose(t));` abomination
            Disposable object = childNode.getObject();
            boolean alreadyDisposed = tree.rememberDisposedTrace(object, trace) != null;
            if (!alreadyDisposed) {
                disposables.add(object);
            }
        });
    }


    @NotNull
    Disposable getObject() {
        return myObject;
    }

    @Override
    public @NonNls String toString() {
        return isRootNode() ? "ROOT" : "Node: " + myObject;
    }

    Throwable getTrace() {
        return myTrace;
    }

    void clearTrace() {
        myTrace = null;
    }

    @TestOnly
    void assertNoReferencesKept(@NotNull Disposable aDisposable) {
        assert getObject() != aDisposable;
        for (ObjectNode node : myChildren.getAllNodes()) {
            node.assertNoReferencesKept(aDisposable);
        }
    }

    ObjectNode findChildNode(@NotNull Disposable object) {
        return myChildren.findChildNode(object);
    }

    @NotNull
    ObjectNode findOrCreateChildNode(@NotNull Disposable object) {
        ObjectNode existing = findChildNode(object);
        if (existing != null) {
            return existing;
        }
        ObjectNode childNode = new ObjectNode(object, isRootNode());
        addChildNode(childNode);
        return childNode;
    }

    // must not override hasCode/equals because ObjectNode must have identity semantics

    private static final class MapNodeChildren implements NodeChildren {
        private final Map<Disposable, ObjectNode> myChildren;

        MapNodeChildren(@NotNull List<ObjectNode> children) {
            Reference2ObjectLinkedOpenHashMap<Disposable, ObjectNode> map = new Reference2ObjectLinkedOpenHashMap<>(children.size());
            for (ObjectNode child : children) {
                map.put(child.getObject(), child);
            }
            myChildren = map;
        }

        @Override
        public @Nullable ObjectNode removeChildNode(@NotNull Disposable object) {
            return myChildren.remove(object);
        }

        @Override
        public @Nullable ObjectNode findChildNode(@NotNull Disposable object) {
            return myChildren.get(object);
        }

        @Override
        public @NotNull NodeChildren addChildNode(@NotNull ObjectNode node) {
            myChildren.put(node.getObject(), node);
            return this;
        }

        @Override
        public void removeChildren(@Nullable Predicate<? super Disposable> condition, @NotNull Consumer<? super ObjectNode> deletedNodeConsumer) {
            Iterator<Map.Entry<Disposable, ObjectNode>> iterator = myChildren.entrySet().iterator();
            List<ObjectNode> deletedValuesToProcess = new SmartList<>();
            while (iterator.hasNext()) {
                Map.Entry<Disposable, ObjectNode> entry = iterator.next();
                if (condition == null || condition.test(entry.getKey())) {
                    ObjectNode value = entry.getValue();
                    iterator.remove();
                    deletedValuesToProcess.add(value);
                }
            }
            for (int i = deletedValuesToProcess.size() - 1; i>= 0; i--) {
                deletedNodeConsumer.accept(deletedValuesToProcess.get(i));
            }
        }

        @Override
        public @NotNull Collection<ObjectNode> getAllNodes() {
            return myChildren.values();
        }
    }

    private static final class ListNodeChildren implements NodeChildren {
        private final @NotNull List<ObjectNode> myChildren;

        ListNodeChildren(@NotNull ObjectNode node) {
            myChildren = new SmartList<>(node);
        }

        @Override
        public ObjectNode removeChildNode(@NotNull Disposable nodeToDelete) {
            List<ObjectNode> children = myChildren;
            // optimization: iterate backwards
            for (int i = children.size() - 1; i >= 0; i--) {
                ObjectNode node = children.get(i);
                if (node.getObject() == nodeToDelete) {
                    return children.remove(i);
                }
            }
            return null;
        }

        @Override
        public @Nullable ObjectNode findChildNode(@NotNull Disposable object) {
            for (ObjectNode node : myChildren) {
                if (node.getObject() == object) {
                    return node;
                }
            }
            return null;
        }

        @Override
        public @NotNull NodeChildren addChildNode(@NotNull ObjectNode node) {
            myChildren.add(node);
            return myChildren.size() > REASONABLY_BIG ? new MapNodeChildren(myChildren) : this;
        }

        @Override
        public void removeChildren(@Nullable Predicate<? super Disposable> condition, @NotNull Consumer<? super ObjectNode> deletedNodeConsumer) {
            for (int i = myChildren.size() - 1; i >= 0; i--) {
                ObjectNode childNode = myChildren.get(i);
                Disposable object = childNode.getObject();
                if (condition == null || condition.test(object)) {
                    myChildren.remove(i);
                    deletedNodeConsumer.accept(childNode);
                }
            }
        }

        @Override
        public @NotNull Collection<ObjectNode> getAllNodes() {
            return myChildren;
        }
    }

    /**
     * A collection of child ObjectNodes.
     * Backed either by an {@code ArrayList<ObjectNode>} (when the number of children is small)
     * or {@code Map<Disposable, ObjectNode>} otherwise, to speed up the lookups.
     */
    private interface NodeChildren {
        @Nullable ObjectNode removeChildNode(@NotNull Disposable object);

        @Nullable ObjectNode findChildNode(@NotNull Disposable object);

        @NotNull // return a new instance of NodeChildren when the underlying data-structure changed, e.g., list->map
        NodeChildren addChildNode(@NotNull ObjectNode node);

        void removeChildren(@Nullable Predicate<? super Disposable> condition, @NotNull Consumer<? super ObjectNode> deletedNodeConsumer);

        @NotNull
        Collection<ObjectNode> getAllNodes();
    }

    private static final NodeChildren EMPTY = new NodeChildren() {
        @Override
        public ObjectNode removeChildNode(@NotNull Disposable object) {
            return null;
        }

        @Override
        public @Nullable ObjectNode findChildNode(@NotNull Disposable object) {
            return null;
        }

        @Override
        public @NotNull NodeChildren addChildNode(@NotNull ObjectNode node) {
            return new ListNodeChildren(node);
        }

        @Override
        public void removeChildren(@Nullable Predicate<? super Disposable> condition, @NotNull Consumer<? super ObjectNode> deletedNodeConsumer) {
        }

        @Override
        public @NotNull Collection<ObjectNode> getAllNodes() {
            return Collections.emptyList();
        }
    };
}
