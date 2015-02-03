// !DIAGNOSTICS: -UNUSED_PARAMETER

<!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>public val a<!> by Delegate()

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}