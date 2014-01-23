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

package org.jetbrains.jet.resolve;

import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetClassInitializer;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveDescriptorRendererTest;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.plugin.project.ResolveElementCache;

public abstract class AbstractAdditionalLazyResolveDescriptorRendererTest extends AbstractLazyResolveDescriptorRendererTest {
    @Override
    protected DeclarationDescriptor getDescriptor(JetDeclaration declaration, ResolveSession resolveSession) {
        if (declaration instanceof JetClassInitializer || JetPsiUtil.isLocal(declaration)) {
            ResolveElementCache resolveElementCache = new ResolveElementCache(resolveSession, getProject());
            return resolveElementCache.resolveToElement(declaration).get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
        }
        return resolveSession.resolveToDescriptor(declaration);
    }
}
