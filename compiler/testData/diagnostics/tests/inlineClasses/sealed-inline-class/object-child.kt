// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC
// !DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

/*
+--------------------------+-------------+-------------+
|parent\child object       |value        |ordinary     |
+==========================+=============+=============+
|sealed value class        |yep          |nope         |
+--------------------------+-------------+-------------+
|value class               |nope         |nope         |
+--------------------------+-------------+-------------+
|inline class              |nope         |nope         |
+--------------------------+-------------+-------------+
|sealed interface          |nope         |OOS          |
+--------------------------+-------------+-------------+
|interface                 |nope         |OOS          |
+--------------------------+-------------+-------------+
|open class                |nope         |OOS          |
+--------------------------+-------------+-------------+
|sealed class              |nope         |OOS          |
+--------------------------+-------------+-------------+
|abstract class            |nope         |OOS          |
+--------------------------+-------------+-------------+
 */

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class SVC

@JvmInline
value class VC(val a: Any)

inline class IC(val a: Any)

sealed interface SI

interface I

open class OC

sealed class SC

abstract class AC


value object VO_SVC: SVC()

<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD!>value<!> object VO_VC: <!FINAL_SUPERTYPE, VALUE_CLASS_CANNOT_EXTEND_CLASSES!>VC<!>("")
<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD!>value<!> object VO_IC: <!FINAL_SUPERTYPE, VALUE_CLASS_CANNOT_EXTEND_CLASSES!>IC<!>("")
<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD!>value<!> object VO_SI: SI
<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD!>value<!> object VO_I: I
<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD!>value<!> object VO_OC: <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>OC<!>()
<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD!>value<!> object VO_SC: <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>SC<!>()
<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD!>value<!> object VO_AC: <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>AC<!>()

<!SEALED_INLINE_CHILD_NOT_VALUE!>object O_SVC<!>: SVC()
object O_VC: <!FINAL_SUPERTYPE!>VC<!>("")
object O_IC: <!FINAL_SUPERTYPE!>IC<!>("")
