// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
private const val KOTLIN_NATIVE_DEFINITIONS_ID = "KND"

abstract class Language(val id: String)

internal class NativeDefinitionsLanguage private constructor() : Language(KOTLIN_NATIVE_DEFINITIONS_ID) {
    companion object {
        val INSTANCE = NativeDefinitionsLanguage()
    }
}

abstract class LanguageFileType(val language: Language)

internal object NativeDefinitionsFileType : LanguageFileType(NativeDefinitionsLanguage.INSTANCE)

abstract class PsiFileBase(val viewProvider: Any, val language: Language)

class NativeDefinitionsFile(viewProvider: Any) : PsiFileBase(viewProvider, NativeDefinitionsLanguage.INSTANCE) {

    fun getFileType(): LanguageFileType = NativeDefinitionsFileType
}


class IFileElementType(language: Language)

private val FILE = IFileElementType(NativeDefinitionsLanguage.INSTANCE)

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, const, functionDeclaration, objectDeclaration,
primaryConstructor, propertyDeclaration, stringLiteral */
