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

package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionDebugInfo;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Set;

public class PrintingConstraintResolutionListener implements ConstraintResolutionListener {

    @Override
    public void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, BoundsOwner typeValue) {
        println("Constraints for " + typeParameterDescriptor);
        printTypeValue(typeValue);
    }

    @Override
    public void constraintsForKnownType(JetType type, BoundsOwner typeValue) {
        println("Constraints for " + type);
        printTypeValue(typeValue);
    }

    @Override
    public void done(ConstraintSystemSolution solution, Set<TypeParameterDescriptor> typeParameterDescriptors) {
        println("==================================================");
        println("");
        println("");
    }

    @Override
    public void log(Object... messageFragments) {
        for (Object fragment : messageFragments) {
            println(fragment);
        }
    }

    @Override
    public void error(Object... messageFragments) {
        for (Object fragment : messageFragments) {
            println(fragment);
        }
    }

    private void printTypeValue(BoundsOwner typeValue) {
        for (BoundsOwner bound : typeValue.getUpperBounds()) {
            println(" :< " + bound);
        }
        for (BoundsOwner bound : typeValue.getLowerBounds()) {
            println(" :> " + bound);
        }
    }

    private static void println(Object message) {
        ResolutionDebugInfo.println(message);
    }
}
