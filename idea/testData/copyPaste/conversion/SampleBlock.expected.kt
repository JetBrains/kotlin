fun main(args: Array<String>) {
    val buffer = getSelectedElements(code.getFile(), code.getStartOffsets(), code.getEndOffsets())

    val project = file.getProject()
    val converter = Converter(project, J2kPackage.getPluginSettings())
    val result = StringBuilder()
    for (e in buffer) {
        result.append(converter.elementToKotlin(e))
    }

    return StringUtil.convertLineSeparators(result.toString())
}