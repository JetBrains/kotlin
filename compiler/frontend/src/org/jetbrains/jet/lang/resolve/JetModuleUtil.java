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

package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.resolve.name.SpecialNames;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.NamespaceType;

public class JetModuleUtil {
    public static NamespaceType getRootNamespaceType(JetElement expression) {
        // TODO: this is a stub: at least the modules' root namespaces must be indexed here
        return new NamespaceType(SpecialNames.ROOT_NAMESPACE, JetScope.EMPTY, ReceiverValue.NO_RECEIVER);
    }
}
