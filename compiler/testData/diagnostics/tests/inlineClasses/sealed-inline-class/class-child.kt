// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC
// !DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

/*
+--------------------------+-------------+-------------+-------------+-------------+
|parent\child class        |value        |ordinary     |inline       |sealed value |
+==========================+=============+=============+=============+=============+
|sealed value class        |yep          |nope         |yep          |yep          |
+--------------------------+-------------+-------------+-------------+-------------+
|value class               |nope         |nope         |nope         |nope         |
+--------------------------+-------------+-------------+-------------+-------------+
|inline class              |nope         |nope         |nope         |nope         |
+--------------------------+-------------+-------------+-------------+-------------+
|sealed interface          |yep          |OOS          |yep          |yep          |
+--------------------------+-------------+-------------+-------------+-------------+
|interface                 |yep          |OOS          |yep          |yep          |
+--------------------------+-------------+-------------+-------------+-------------+
|open class                |nope         |OOS          |nope         |nope         |
+--------------------------+-------------+-------------+-------------+-------------+
|sealed class              |nope         |OOS          |nope         |nope         |
+--------------------------+-------------+-------------+-------------+-------------+
|abstract class            |nope         |OOS          |nope         |nope         |
+--------------------------+-------------+-------------+-------------+-------------+
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


@JvmInline
value class VC_SVC(val a: Int): SVC()
@JvmInline
value class VC_SI(val a: Any): SI
@JvmInline
value class VC_I(val a: Any): I

@JvmInline
value class VC_VC(val b: Any): <!FINAL_SUPERTYPE, VALUE_CLASS_CANNOT_EXTEND_CLASSES!>VC<!>(b)
@JvmInline
value class VC_IC(val b: Any): <!FINAL_SUPERTYPE, VALUE_CLASS_CANNOT_EXTEND_CLASSES!>IC<!>(b)
@JvmInline
value class VC_OC(val a: Any): <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>OC<!>()
@JvmInline
value class VC_SC(val a: Any): <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>SC<!>()
@JvmInline
value class VC_AC(val a: Any): <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>AC<!>()

<!SEALED_INLINE_CHILD_NOT_VALUE!>class C_SVC: SVC()<!>
class C_VC: <!FINAL_SUPERTYPE!>VC<!>("")
class C_IC: <!FINAL_SUPERTYPE!>IC<!>("")


inline class IC_SVC(val a: String): SVC()
inline class IC_SI(val a: Any): SI
inline class IC_I(val a: Any): I

inline class IC_VC(val b: Any): <!FINAL_SUPERTYPE, VALUE_CLASS_CANNOT_EXTEND_CLASSES!>VC<!>(b)
inline class IC_IC(val b: Any): <!FINAL_SUPERTYPE, VALUE_CLASS_CANNOT_EXTEND_CLASSES!>IC<!>(b)
inline class IC_OC(val a: Any): <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>OC<!>()
inline class IC_SC(val a: Any): <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>SC<!>()
inline class IC_AC(val a: Any): <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>AC<!>()


@JvmInline
sealed value class SVC_SVC: SVC()
@JvmInline
sealed value class SVC_SI: SI
@JvmInline
sealed value class SVC_I: I

@JvmInline
sealed value class SVC_VC: <!FINAL_SUPERTYPE, VALUE_CLASS_CANNOT_EXTEND_CLASSES!>VC<!>("")
@JvmInline
sealed value class SVC_IC: <!FINAL_SUPERTYPE, VALUE_CLASS_CANNOT_EXTEND_CLASSES!>IC<!>("")
@JvmInline
sealed value class SVC_OC: <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>OC<!>()
@JvmInline
sealed value class SVC_SC: <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>SC<!>()
@JvmInline
sealed value class SVC_AC: <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>AC<!>()
