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
    public void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, ConstraintSystemImpl.TypeValue typeValue) {
        println("Constraints for " + typeParameterDescriptor);
        printTypeValue(typeValue);
    }

    @Override
    public void constraintsForKnownType(JetType type, ConstraintSystemImpl.TypeValue typeValue) {
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
    public void log(Object message) {
        println(message);
    }

    @Override
    public void error(Object message) {
        println(message);
    }

    private void printTypeValue(ConstraintSystemImpl.TypeValue typeValue) {
        for (ConstraintSystemImpl.TypeValue bound : typeValue.getUpperBounds()) {
            println(" :< " + bound);
        }
        for (ConstraintSystemImpl.TypeValue bound : typeValue.getLowerBounds()) {
            println(" :> " + bound);
        }
    }

    private static void println(Object message) {
        ResolutionDebugInfo.println(message);
    }
}
