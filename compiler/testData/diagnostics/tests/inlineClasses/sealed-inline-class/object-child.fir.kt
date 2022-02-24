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

value object VO_VC: VC("")
value object VO_IC: IC("")
value object VO_SI: SI
value object VO_I: I
value object VO_OC: OC()
value object VO_SC: SC()
value object VO_AC: AC()

object O_SVC: SVC()
object O_VC: VC("")
object O_IC: IC("")