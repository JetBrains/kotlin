var <info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_PACKAGE_PROPERTY">x</info></info> = 5

val <info textAttributesKey="KOTLIN_CLASS">Int</info>.<info textAttributesKey="KOTLIN_EXTENSION_PROPERTY">sq</info> : <info textAttributesKey="KOTLIN_CLASS">Int</info>
<info textAttributesKey="KOTLIN_KEYWORD">get</info>() {
  return this * this
}

val <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY">y</info> : <info textAttributesKey="KOTLIN_CLASS">Int</info> = 1
<info textAttributesKey="KOTLIN_KEYWORD">get</info>() {
  return 5.<info textAttributesKey="KOTLIN_EXTENSION_PROPERTY">sq</info> + <info textAttributesKey="KOTLIN_BACKING_FIELD_VARIABLE">field</info> + <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">x</info></info>
}

class <info textAttributesKey="KOTLIN_CLASS">Foo</info>(
    val <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">a</info> : <info textAttributesKey="KOTLIN_CLASS">Int</info>,
    <info textAttributesKey="KOTLIN_PARAMETER">b</info> : <info textAttributesKey="KOTLIN_CLASS">String</info>,
    var <info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">c</info></info> : <info textAttributesKey="KOTLIN_CLASS">String</info>
) {
  <info>init</info> {
    <info textAttributesKey="KOTLIN_PARAMETER">b</info>
  }

  fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">f</info>(<info textAttributesKey="KOTLIN_PARAMETER">p</info> : <info textAttributesKey="KOTLIN_CLASS">Int</info> = <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">a</info>) {}

  var <info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">v</info></info> : <info textAttributesKey="KOTLIN_CLASS">Int</info>
  <info textAttributesKey="KOTLIN_KEYWORD">get</info>() {
    return 1;
  }
  <info textAttributesKey="KOTLIN_KEYWORD">set</info>(<info textAttributesKey="KOTLIN_PARAMETER">value</info>) {
    <info textAttributesKey="KOTLIN_PARAMETER">value</info>
  }
}

// NO_CHECK_WARNINGS