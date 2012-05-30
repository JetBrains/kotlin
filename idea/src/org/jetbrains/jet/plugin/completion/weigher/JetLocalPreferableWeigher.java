/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion.weigher;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementWeigher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.LocalVariableDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.plugin.completion.JetLookupObject;

/**
 * @author Nikolay Krasko
 */
class JetLocalPreferableWeigher extends LookupElementWeigher {
    JetLocalPreferableWeigher() {
        super("JetLocalElementWeigher");
    }

    private enum MyResult {
        probableKeyword,
        localOrParameter,
        normal,
        packages
    }

    @NotNull
    @Override
    public MyResult weigh(@NotNull LookupElement element) {
        Object object = element.getObject();
        if (object instanceof JetLookupObject) {
            JetLookupObject lookupObject = (JetLookupObject) object;
            DeclarationDescriptor descriptor = lookupObject.getDescriptor();
            if (descriptor != null) {
                if (descriptor instanceof LocalVariableDescriptor || descriptor instanceof ValueParameterDescriptor) {
                    return MyResult.localOrParameter;
                }
                if (descriptor instanceof NamespaceDescriptor) {
                    return MyResult.packages;
                }
            }
        } else if (object instanceof String) {
             return MyResult.probableKeyword;
        }

        return MyResult.normal;
    }
}
