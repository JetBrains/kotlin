package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.calls.ResolutionDebugInfo;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Set;

/**
 * @author abreslav
 */
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
