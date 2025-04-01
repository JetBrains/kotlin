class TopLevelClass {
    @ClassLevel1(
        value = {
            @ClassClassAnnotation
            class ClassLocalClass {}
        }
    )
    @ClassLevel2(
        value = {
            @ClassFunctionAnnotation
            fun classLocalFunction() = 1
        }
    )
    @ClassLevel3(
        value = { @ClassPropertyAnnotation val classLocalProperty = "str" }
    )
    @ClassLevel4(
        value = {
            @ClassTypeAlias
            typealias ClassLocalTypeAlias = String
        }
    )
}

@FileLevel1(value = {
    @FileClassAnnotation
    class FileLocalClass {}
})
@FileLevel2(value = {
    @FileFunctionAnnotation
    fun fileLocalFunction() = 1
})
@FileLevel3(value = { @FilePropertyAnnotation val fileLocalProperty = "str" })
@FileLevel4(value = {
    @FileTypeAlias
    typealias FileLocalTypeAlias = Int
})