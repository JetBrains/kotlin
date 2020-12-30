// !DIAGNOSTICS: -PLATFORM_CLASS_MAPPED_TO_KOTLIN -UNUSED_PARAMETER -ABSTRACT_MEMBER_NOT_IMPLEMENTED -USELESS_CAST
import java.lang.CharSequence as JCS

class JSub: JCS
class Sub: CharSequence

fun test(
  s: Sub,
  js: JSub,
  cs: CharSequence,
  jcs: JCS
) {
  // js as CharSequence // - this case is not supported due to limitation in PlatformToKotlinClassMap
  js as JCS

  s as CharSequence
  s as JCS

  js as Sub
  s as JSub
}
