/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.utils;

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DFS {
    public static <N, R> R dfs(@NotNull Collection<N> nodes, @NotNull Neighbors<N> neighbors, @NotNull Visited<N> visited, @NotNull NodeHandler<N, R> handler) {
        for (N node : nodes) {
            doDfs(node, neighbors, visited, handler);
        }
        return handler.result();
    }

    public static <N, R> R dfs(
        @NotNull Collection<N> nodes,
        @NotNull Neighbors<N> neighbors,
        @NotNull NodeHandler<N, R> handler
    ) {
        return dfs(nodes, neighbors, new VisitedWithSet<N>(), handler);
    }

    public static <N> Boolean ifAny(
            @NotNull Collection<N> nodes,
            @NotNull Neighbors<N> neighbors,
            @NotNull final Function1<N, Boolean> predicate
    ) {
        final boolean[] result = new boolean[1];

        return dfs(nodes, neighbors, new AbstractNodeHandler<N, Boolean>() {
            @Override
            public boolean beforeChildren(N current) {
                if (predicate.invoke(current)) {
                    result[0] = true;
                }

                return !result[0];
            }

            @Override
            public Boolean result() {
                return result[0];
            }
        });
    }

    public static <N, R> R dfsFromNode(@NotNull N node, @NotNull Neighbors<N> neighbors, @NotNull Visited<N> visited, @NotNull NodeHandler<N, R> handler) {
        doDfs(node, neighbors, visited, handler);
        return handler.result();
    }

    public static <N> void dfsFromNode(
            @NotNull N node,
            @NotNull Neighbors<N> neighbors,
            @NotNull Visited<N> visited
    ) {
        dfsFromNode(node, neighbors, visited, new AbstractNodeHandler<N, Void>() {
            @Override
            public Void result() {
                return null;
            }
        });
    }

    public static <N> List<N> topologicalOrder(@NotNull Iterable<N> nodes, @NotNull Neighbors<N> neighbors, @NotNull Visited<N> visited) {
        TopologicalOrder<N> handler = new TopologicalOrder<N>();
        for (N node : nodes) {
            doDfs(node, neighbors, visited, handler);
        }
        return handler.result();
    }

    public static <N> List<N> topologicalOrder(@NotNull Iterable<N> nodes, @NotNull Neighbors<N> neighbors) {
        return topologicalOrder(nodes, neighbors, new VisitedWithSet<N>());
    }

    public static <N> void doDfs(@NotNull N current, @NotNull Neighbors<N> neighbors, @NotNull Visited<N> visited, @NotNull NodeHandler<N, ?> handler) {
        if (!visited.checkAndMarkVisited(current)) return;
        if (!handler.beforeChildren(current)) return;

        for (N neighbor : neighbors.getNeighbors(current)) {
            doDfs(neighbor, neighbors, visited, handler);
        }
        handler.afterChildren(current);
    }

    public interface NodeHandler<N, R> {
        boolean beforeChildren(N current);

        void afterChildren(N current);

        R result();
    }

    public interface Neighbors<N> {
        @NotNull
        Iterable<? extends N> getNeighbors(N current);
    }

    public interface Visited<N> {
        boolean checkAndMarkVisited(N current);
    }

    public static abstract class AbstractNodeHandler<N, R> implements NodeHandler<N, R> {
        @Override
        public boolean beforeChildren(N current) {
            return true;
        }

        @Override
        public void afterChildren(N current) {
        }
    }

    public static class VisitedWithSet<N> implements Visited<N> {
        private final Set<N> visited;

        public VisitedWithSet() {
            this(new HashSet<N>());
        }

        public VisitedWithSet(@NotNull Set<N> visited) {
            this.visited = visited;
        }

        @Override
        public boolean checkAndMarkVisited(N current) {
            return visited.add(current);
        }
    }

    public static abstract class CollectingNodeHandler<N, R, C extends Iterable<R>> extends AbstractNodeHandler<N, C> {
        @NotNull
        protected final C result;

        protected CollectingNodeHandler(@NotNull C result) {
            this.result = result;
        }

        @Override
        @NotNull
        public C result() {
            return result;
        }
    }

    public static abstract class NodeHandlerWithListResult<N, R> extends CollectingNodeHandler<N, R, LinkedList<R>> {
        protected NodeHandlerWithListResult() {
            super(new LinkedList<R>());
        }
    }

    public static class TopologicalOrder<N> extends NodeHandlerWithListResult<N, N> {
        @Override
        public void afterChildren(N current) {
            result.addFirst(current);
        }
    }
}
