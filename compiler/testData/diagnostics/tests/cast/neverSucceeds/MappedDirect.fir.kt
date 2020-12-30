// !DIAGNOSTICS: -PLATFORM_CLASS_MAPPED_TO_KOTLIN -USELESS_CAST
import java.lang.String as JString
import java.lang.CharSequence as JCS

fun test(
  s: String,
  js: JString,
  cs: CharSequence,
  jcs: JCS
) {
  s as JString
  s as JCS
  s as CharSequence
  s as String

  js as JString
  js as JCS
  js as CharSequence
  js as String

  cs as JString
  cs as JCS
  cs as CharSequence
  cs as String

  jcs as JString
  jcs as JCS
  jcs as CharSequence
  jcs as String

  jcs as Int
  s as java.lang.Integer
}
