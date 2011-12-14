package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetTypeParameter;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * @author alex.tkachman
 */
public class SignatureUtil {
    private SignatureUtil() {
    }

    public static String classToSignature(JetClass type, BindingContext bindingContext, JetTypeMapper typeMapper) {
        StringBuilder sb = new StringBuilder();
        genTypeParams(type, sb);
        List<JetDelegationSpecifier> delegationSpecifiers = type.getDelegationSpecifiers();
        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            JetTypeReference typeReference = specifier.getTypeReference();
            JetType jetType = bindingContext.get(BindingContext.TYPE, typeReference);
            genJetType(sb, jetType, typeMapper);
        }
        return sb.toString();
    }

    private static void genJetType(StringBuilder sb, JetType jetType, JetTypeMapper typeMapper) {
        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if(descriptor instanceof ClassDescriptor) {
            JetType defaultType = ((ClassDescriptor) descriptor).getDefaultType();
            Type type = typeMapper.mapType(defaultType, OwnerKind.IMPLEMENTATION);
            if(JetTypeMapper.isPrimitive(type)) {
                type = JetTypeMapper.boxType(type);
            }
            sb.append(type.getDescriptor());
        }
        else {
            sb.append("T");
            JetType defaultType = ((ClassDescriptor) descriptor.getContainingDeclaration()).getDefaultType();
            Type type = typeMapper.mapType(defaultType, OwnerKind.IMPLEMENTATION);
            sb.append(type.getDescriptor());
            sb.append(descriptor.getName());
            sb.append(";");
        }
        if(!jetType.getArguments().isEmpty()) {
            sb.append("<");
            for (TypeProjection typeProjection : jetType.getArguments()) {
                if(typeProjection.getProjectionKind() == Variance.IN_VARIANCE)
                    sb.append("in ");
                if(typeProjection.getProjectionKind() == Variance.OUT_VARIANCE)
                    sb.append("out ");
                genJetType(sb, typeProjection.getType(), typeMapper);
            }
            sb.append(">");
        }
        if(jetType.isNullable())
            sb.append("?");
    }

    private static void genTypeParams(JetClass type, StringBuilder sb) {
        List<JetTypeParameter> parameters = type.getTypeParameterList().getParameters();
        if (parameters.isEmpty()) {
            return;
        }
        sb.append('<');
        for(JetTypeParameter param : parameters) {
            Variance variance = param.getVariance();
            if(variance == Variance.IN_VARIANCE)
                sb.append("in ");
            else if(variance == Variance.OUT_VARIANCE)
                sb.append("out ");
            sb.append(param.getName());
            sb.append(";");
        }
        sb.append('>');
    }
}
