/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.extensions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;

import java.lang.ref.WeakReference;
import java.util.*;

public class CallResolverExtensionProvider {

    private final static CompositeExtension DEFAULT =
            new CompositeExtension(Arrays.asList(
                    new NeedSyntheticCallResolverExtension(),
                    new ReifiedTypeParameterSubstitutionCheck(),
                    new CapturingInClosureExtension()
            ));

    private WeakReference<Map<DeclarationDescriptor, List<CallResolverExtension>>> extensionsCache;

    @NotNull
    public CallResolverExtension createExtension(@Nullable DeclarationDescriptor descriptor, boolean isAnnotationContext) {
        if (descriptor == null || isAnnotationContext) {
            return DEFAULT;
        }
        return new CompositeExtension(createExtensions(descriptor));
    }

    // create extension list with default one at the end
    @NotNull
    private List<CallResolverExtension> createExtensions(@NotNull DeclarationDescriptor declaration) {
        Map<DeclarationDescriptor, List<CallResolverExtension>> map;
        if (extensionsCache == null || (map = extensionsCache.get()) == null) {
            map = new HashMap<DeclarationDescriptor, List<CallResolverExtension>>();
            extensionsCache = new WeakReference<Map<DeclarationDescriptor, List<CallResolverExtension>>>(map);
        }

        List<CallResolverExtension> extensions = map.get(declaration);
        if (extensions != null) {
            return extensions;
        }

        extensions = new ArrayList<CallResolverExtension>();

        DeclarationDescriptor parent = declaration.getContainingDeclaration();
        if (parent != null) {
            extensions.addAll(createExtensions(parent));
            extensions.remove(extensions.size() - 1);//remove default from parent list
        }

        appendExtensionsFor(declaration, extensions);

        List<CallResolverExtension> immutableResult = Collections.unmodifiableList(extensions);
        map.put(declaration, immutableResult);

        return immutableResult;
    }

    // with default one at the end
    private static void appendExtensionsFor(DeclarationDescriptor declarationDescriptor, List<CallResolverExtension> extensions) {
        if (declarationDescriptor instanceof SimpleFunctionDescriptor) {
            SimpleFunctionDescriptor descriptor = (SimpleFunctionDescriptor) declarationDescriptor;
            if (descriptor.getInlineStrategy().isInline()) {
                extensions.add(new InlineCallResolverExtension(descriptor));
            }
        }
        // add your extensions here
        extensions.add(DEFAULT);
    }
}
