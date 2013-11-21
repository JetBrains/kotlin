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

import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;

import java.util.Comparator;
import java.util.List;

public class MemberComparator implements Comparator<DeclarationDescriptor> {
    public static final MemberComparator INSTANCE = new MemberComparator();

    private static final DescriptorRenderer RENDERER = new DescriptorRendererBuilder().setWithDefinedIn(false).setVerbose(true).build();

    private MemberComparator() {
    }

    private static int getDeclarationPriority(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ConstructorDescriptor) {
            return 6;
        }
        else if (descriptor instanceof PropertyDescriptor) {
            if (((PropertyDescriptor)descriptor).getReceiverParameter() == null) {
                return 5;
            }
            else {
                return 4;
            }
        }
        else if (descriptor instanceof FunctionDescriptor) {
            if (((FunctionDescriptor)descriptor).getReceiverParameter() == null) {
                return 3;
            }
            else {
                return 2;
            }
        }
        else if (descriptor instanceof ClassDescriptor) {
            return 1;
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
            assert false : "Descriptors must be callable:\n" + o1 + "\n" + o2;
        }

        CallableDescriptor c1 = (CallableDescriptor)o1;
        CallableDescriptor c2 = (CallableDescriptor)o2;

        ReceiverParameterDescriptor c1ReceiverParameter = c1.getReceiverParameter();
        ReceiverParameterDescriptor c2ReceiverParameter = c2.getReceiverParameter();
        assert (c1ReceiverParameter != null) == (c2ReceiverParameter != null);
        if (c1ReceiverParameter != null) {
            String r1 = RENDERER.renderType(c1ReceiverParameter.getType());
            String r2 = RENDERER.renderType(c2ReceiverParameter.getType());
            int receiversCompareTo = r1.compareTo(r2);
            if (receiversCompareTo != 0) {
                return receiversCompareTo;
            }
        }

        List<ValueParameterDescriptor> c1ValueParameters = c1.getValueParameters();
        List<ValueParameterDescriptor> c2ValueParameters = c2.getValueParameters();
        for (int i = 0; i < Math.min(c1ValueParameters.size(), c2ValueParameters.size()); i++) {
            String p1 = RENDERER.renderType(c1ValueParameters.get(i).getType());
            String p2 = RENDERER.renderType(c2ValueParameters.get(i).getType());
            int parametersCompareTo = p1.compareTo(p2);
            if (parametersCompareTo != 0) {
                return parametersCompareTo;
            }
        }

        int valueParametersNumberCompareTo = c1ValueParameters.size() - c2ValueParameters.size();
        if (valueParametersNumberCompareTo != 0) {
            return valueParametersNumberCompareTo;
        }

        List<TypeParameterDescriptor> c1TypeParameters = c1.getTypeParameters();
        List<TypeParameterDescriptor> c2TypeParameters = c2.getTypeParameters();
        for (int i = 0; i < Math.min(c1TypeParameters.size(), c2TypeParameters.size()); i++) {
            String p1 = RENDERER.renderType(c1TypeParameters.get(i).getUpperBoundsAsType());
            String p2 = RENDERER.renderType(c2TypeParameters.get(i).getUpperBoundsAsType());
            int parametersCompareTo = p1.compareTo(p2);
            if (parametersCompareTo != 0) {
                return parametersCompareTo;
            }
        }

        int typeParametersCompareTo = c1TypeParameters.size() - c2TypeParameters.size();
        if (typeParametersCompareTo != 0) {
            return typeParametersCompareTo;
        }

        if (c1 instanceof CallableMemberDescriptor && c2 instanceof CallableMemberDescriptor) {
            CallableMemberDescriptor.Kind c1Kind = ((CallableMemberDescriptor) c1).getKind();
            CallableMemberDescriptor.Kind c2Kind = ((CallableMemberDescriptor) c2).getKind();
            int kindsCompareTo = c1Kind.ordinal() - c2Kind.ordinal();
            if (kindsCompareTo != 0) {
                return kindsCompareTo;
            }
        }

        return RENDERER.render(o1).compareTo(RENDERER.render(o2));
    }
}
