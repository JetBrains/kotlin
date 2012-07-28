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

package org.jetbrains.jet.codegen;

import com.google.common.collect.Lists;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author max
 */
public class FrameMap {
    private final TObjectIntHashMap<DeclarationDescriptor> myVarIndex = new TObjectIntHashMap<DeclarationDescriptor>();
    private final TObjectIntHashMap<DeclarationDescriptor> myVarSizes = new TObjectIntHashMap<DeclarationDescriptor>();
    private int myMaxIndex = 0;

    public void enter(DeclarationDescriptor descriptor, int size) {
        myVarIndex.put(descriptor, myMaxIndex);
        myMaxIndex += size;
        myVarSizes.put(descriptor, size);
    }

    public int leave(DeclarationDescriptor descriptor) {
        int size = myVarSizes.get(descriptor);
        myMaxIndex -= size;
        myVarSizes.remove(descriptor);
        int oldIndex = myVarIndex.remove(descriptor);
        if (oldIndex != myMaxIndex) {
            throw new IllegalStateException("descriptor can be left only if it is last");
        }
        return oldIndex;
    }

    public int enterTemp() {
        return enterTemp(1);
    }

    public int enterTemp(int size) {
        int result = myMaxIndex;
        myMaxIndex += size;
        return result;
    }

    public void leaveTemp() {
        myMaxIndex--;
    }

    public void leaveTemp(int size) {
        myMaxIndex -= size;
    }

    public int getIndex(DeclarationDescriptor descriptor) {
        return myVarIndex.contains(descriptor) ? myVarIndex.get(descriptor) : -1;
    }

    public Mark mark() {
        return new Mark(myMaxIndex);
    }

    public class Mark {
        private final int myIndex;

        public Mark(int index) {
            myIndex = index;
        }

        public void dropTo() {
            List<DeclarationDescriptor> descriptorsToDrop = new ArrayList<DeclarationDescriptor>();
            TObjectIntIterator<DeclarationDescriptor> iterator = myVarIndex.iterator();
            while (iterator.hasNext()) {
                iterator.advance();
                if (iterator.value() >= myIndex) {
                    descriptorsToDrop.add(iterator.key());
                }
            }
            for (DeclarationDescriptor declarationDescriptor : descriptorsToDrop) {
                myVarIndex.remove(declarationDescriptor);
                myVarSizes.remove(declarationDescriptor);
            }
            myMaxIndex = myIndex;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (myVarIndex.size() != myVarSizes.size()) {
            return "inconsistent";
        }

        class Tuple3<A, B, C> {
            private final A _1;
            private final B _2;
            private final C _3;

            Tuple3(A _1, B _2, C _3) {
                this._1 = _1;
                this._2 = _2;
                this._3 = _3;
            }
        }

        List<Tuple3<DeclarationDescriptor, Integer, Integer>> descriptors = Lists.newArrayList();

        for (Object descriptor0 : myVarIndex.keys()) {
            DeclarationDescriptor descriptor = (DeclarationDescriptor) descriptor0;
            int varIndex = myVarIndex.get(descriptor);
            int varSize = myVarSizes.get(descriptor);
            descriptors.add(new Tuple3<DeclarationDescriptor, Integer, Integer>(descriptor, varIndex, varSize));
        }

        Collections.sort(descriptors, new Comparator<Tuple3<DeclarationDescriptor, Integer, Integer>>() {
            @Override
            public int compare(Tuple3<DeclarationDescriptor, Integer, Integer> left, Tuple3<DeclarationDescriptor, Integer, Integer> right) {
                return left._2 - right._2;
            }
        });

        sb.append("size=" + myMaxIndex);

        boolean first = true;
        for (Tuple3<DeclarationDescriptor, Integer, Integer> t : descriptors) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(t._1 + ",i=" + t._2 + ",s=" + t._3);
        }

        return sb.toString();
    }
}
