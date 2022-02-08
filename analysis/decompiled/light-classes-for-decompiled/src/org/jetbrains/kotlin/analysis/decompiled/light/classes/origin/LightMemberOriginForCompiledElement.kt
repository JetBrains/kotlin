package org.jetbrains.kotlin.analysis.decompiled.light.classes.origin


import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.type.MapPsiToAsmDesc

interface LightMemberOriginForCompiledElement<T : PsiMember> : LightMemberOrigin {
    val member: T

    override val originKind: JvmDeclarationOriginKind
        get() = JvmDeclarationOriginKind.OTHER

    override fun isEquivalentTo(other: PsiElement?): Boolean {
        return when (other) {
            is KtDeclaration -> originalElement?.isEquivalentTo(other) ?: false
            is PsiMember -> member.isEquivalentTo(other)
            else -> false
        }
    }

    override fun isValid(): Boolean = member.isValid
}


data class LightMemberOriginForCompiledField(val psiField: PsiField, val file: KtClsFile) : LightMemberOriginForCompiledElement<PsiField> {
    override val member: PsiField
        get() = psiField

    override fun copy(): LightMemberOrigin {
        return LightMemberOriginForCompiledField(psiField.copy() as PsiField, file)
    }

    override fun isEquivalentTo(other: LightMemberOrigin?): Boolean {
        if (other !is LightMemberOriginForCompiledField) return false
        return psiField.isEquivalentTo(other.psiField)
    }

    override val originalElement: KtDeclaration? by lazyPub {
        val desc = MapPsiToAsmDesc.typeDesc(psiField.type)
        val signature = MemberSignature.fromFieldNameAndDesc(psiField.name, desc)
        KotlinDeclarationInCompiledFileSearcher.getInstance().findDeclarationInCompiledFile(file, psiField, signature)
    }
}

data class LightMemberOriginForCompiledMethod(val psiMethod: PsiMethod, val file: KtClsFile) :
    LightMemberOriginForCompiledElement<PsiMethod> {

    override val member: PsiMethod
        get() = psiMethod

    override fun isEquivalentTo(other: LightMemberOrigin?): Boolean {
        if (other !is LightMemberOriginForCompiledMethod) return false
        return psiMethod.isEquivalentTo(other.psiMethod)
    }

    override fun copy(): LightMemberOrigin {
        return LightMemberOriginForCompiledMethod(psiMethod.copy() as PsiMethod, file)
    }

    override val originalElement: KtDeclaration? by lazyPub {
        val desc = MapPsiToAsmDesc.methodDesc(psiMethod)
        val name = if (psiMethod.isConstructor) "<init>" else psiMethod.name
        val signature = MemberSignature.fromMethodNameAndDesc(name, desc)
        KotlinDeclarationInCompiledFileSearcher.getInstance().findDeclarationInCompiledFile(file, psiMethod, signature)
    }
}