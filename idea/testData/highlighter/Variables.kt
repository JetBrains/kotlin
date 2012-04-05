var <info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_PROPERTY_WITH_BACKING_FIELD"><info textAttributesKey="KOTLIN_NAMESPACE_PROPERTY">x</info></info></info> = 5

val Int.<info textAttributesKey="KOTLIN_EXTENSION_PROPERTY">sq</info> : Int
<info textAttributesKey="KOTLIN_KEYWORD">get</info>() {
  return this * this
}

val <info textAttributesKey="KOTLIN_NAMESPACE_PROPERTY"><info textAttributesKey="KOTLIN_PROPERTY_WITH_BACKING_FIELD">y</info></info> : Int = 1
<info textAttributesKey="KOTLIN_KEYWORD">get</info>() {
  return 5.<info textAttributesKey="KOTLIN_EXTENSION_PROPERTY">sq</info> + <info textAttributesKey="KOTLIN_NAMESPACE_PROPERTY"><info textAttributesKey="KOTLIN_BACKING_FIELD_ACCESS">$y</info></info> + <info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_NAMESPACE_PROPERTY">x</info></info>
}

class <info textAttributesKey="KOTLIN_CLASS">Foo</info>(val <info textAttributesKey="KOTLIN_PARAMETER"><info textAttributesKey="KOTLIN_INSTANCE_PROPERTY"><info textAttributesKey="KOTLIN_PROPERTY_WITH_BACKING_FIELD">a</info></info></info> : Int, <info textAttributesKey="KOTLIN_PARAMETER">b</info> : String) {
  {
    <info textAttributesKey="KOTLIN_PARAMETER">b</info>
  }

  var <info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">v</info></info> : Int
  <info textAttributesKey="KOTLIN_KEYWORD">get</info>() {
    return 1;
  }
  <info textAttributesKey="KOTLIN_KEYWORD">set</info>(<info textAttributesKey="KOTLIN_PARAMETER">value</info>) {
    <info textAttributesKey="KOTLIN_PARAMETER">value</info>
  }
}