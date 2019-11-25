// EXPECTED_DUPLICATED_HIGHLIGHTING

val <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY">packageSize</info> = 0
val <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION"><info textAttributesKey="KOTLIN_PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION">packageSizeGetter</info></info>
<info textAttributesKey="KOTLIN_KEYWORD">get</info>() = <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY">packageSize</info> * 2

var <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION">packageSizeSetter</info></info></info> = 5
<info textAttributesKey="KOTLIN_KEYWORD">set</info>(<info textAttributesKey="KOTLIN_PARAMETER">value</info>) {
    <info textAttributesKey="KOTLIN_BACKING_FIELD_VARIABLE"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">field</info></info> = <info textAttributesKey="KOTLIN_PARAMETER">value</info> * 2
}

var <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION"><info textAttributesKey="KOTLIN_PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">packageSizeBean</info></info></info> = 5
<info textAttributesKey="KOTLIN_KEYWORD">get</info>() = <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY">packageSize</info> * 2
<info textAttributesKey="KOTLIN_KEYWORD">set</info>(<info textAttributesKey="KOTLIN_PARAMETER">value</info>) {
    <info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_BACKING_FIELD_VARIABLE">field</info></info> = <info textAttributesKey="KOTLIN_PARAMETER">value</info> * 2
}


class <info textAttributesKey="KOTLIN_CLASS">test</info>() {
    // no highlighting check
    val <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">size</info> = 0

    val <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">classSize</info> = 0

    val <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION"><info textAttributesKey="KOTLIN_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION">classSizeGetter</info></info>
    <info textAttributesKey="KOTLIN_KEYWORD">get</info>() = <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">classSize</info> * 2

    var <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION"><info textAttributesKey="KOTLIN_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">classSizeSetter</info></info></info> = 5
    <info textAttributesKey="KOTLIN_KEYWORD">set</info>(<info textAttributesKey="KOTLIN_PARAMETER">value</info>) {
        <info textAttributesKey="KOTLIN_BACKING_FIELD_VARIABLE"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">field</info></info> = <info textAttributesKey="KOTLIN_PARAMETER">value</info> * 2
    }

    var <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION">classSizeBean</info></info></info> = 5
    <info textAttributesKey="KOTLIN_KEYWORD">get</info>() = <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">classSize</info> * 2
    <info textAttributesKey="KOTLIN_KEYWORD">set</info>(<info textAttributesKey="KOTLIN_PARAMETER">value</info>) {
        <info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_BACKING_FIELD_VARIABLE">field</info></info> = <info textAttributesKey="KOTLIN_PARAMETER">value</info> * 2
    }

    fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">callCustomPD</info>() {
        <info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION"><info textAttributesKey="KOTLIN_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION">classSizeBean</info></info></info> = 30
    }
}
