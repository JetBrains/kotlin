// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
data class ConverterSettings(
    var forceNotNullTypes: Boolean,
    var specifyLocalVariableTypeByDefault: Boolean,
    var specifyFieldTypeByDefault: Boolean,
    var openByDefault: Boolean,
    var publicByDefault: Boolean,
    // TODO KTIJ-29063
    // In the basic mode, only essential conversions/processings are performed
    var basicMode: Boolean,
) {

    companion object {
        val defaultSettings: ConverterSettings = ConverterSettings(
            forceNotNullTypes = true,
            specifyLocalVariableTypeByDefault = false,
            specifyFieldTypeByDefault = false,
            openByDefault = false,
            publicByDefault = false,
            basicMode = false,
        )

        val publicByDefault: ConverterSettings = defaultSettings.copy(publicByDefault = true)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, objectDeclaration, primaryConstructor,
propertyDeclaration */
