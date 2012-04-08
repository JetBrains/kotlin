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

package org.jetbrains.jet.plugin.libraries;

import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Comparator;

/**
* @author Evgeny Gerashchenko
* @since 4/8/12
*/
class DeclarationDescriptorComparator implements Comparator<DeclarationDescriptor> {
    private static int getDeclarationPriority(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            return 4;
        } else if (descriptor instanceof PropertyDescriptor) {
            return 3;
        } else if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor fun = (FunctionDescriptor)descriptor;
            if (fun.getReceiverParameter() == ReceiverDescriptor.NO_RECEIVER) {
                return 2;
            } else {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int compare(DeclarationDescriptor o1, DeclarationDescriptor o2) {
        int prioritiesCompareTo = getDeclarationPriority(o2) - getDeclarationPriority(o1);
        if (prioritiesCompareTo != 0) {
            return prioritiesCompareTo;
        }

        int namesCompareTo = o1.getName().compareTo(o2.getName());
        if (namesCompareTo != 0) {
            return namesCompareTo;
        }

        if (!(o1 instanceof CallableDescriptor) || !(o2 instanceof CallableDescriptor)) {
            assert false;
        }

        CallableDescriptor c1 = (CallableDescriptor)o1;
        CallableDescriptor c2 = (CallableDescriptor)o2;

        if (c1.getReceiverParameter() != ReceiverDescriptor.NO_RECEIVER && c2.getReceiverParameter() != ReceiverDescriptor.NO_RECEIVER) {
            String r1 = DescriptorRenderer.TEXT.renderType(c1.getReceiverParameter().getType());
            String r2 = DescriptorRenderer.TEXT.renderType(c2.getReceiverParameter().getType());
            int receiversCompareTo = r1.compareTo(r2);
            if (receiversCompareTo != 0) {
                return receiversCompareTo;
            }
        }

        for (int i = 0; i < Math.min(c1.getValueParameters().size(), c2.getValueParameters().size()); i++) {
            String p1 = DescriptorRenderer.TEXT.renderType(c1.getValueParameters().get(i).getType());
            String p2 = DescriptorRenderer.TEXT.renderType(c2.getValueParameters().get(i).getType());
            int parametersCompareTo = p1.compareTo(p2);
            if (parametersCompareTo != 0) {
                return parametersCompareTo;
            }
        }

        return c1.getValueParameters().size() - c2.getValueParameters().size();
    }
}
