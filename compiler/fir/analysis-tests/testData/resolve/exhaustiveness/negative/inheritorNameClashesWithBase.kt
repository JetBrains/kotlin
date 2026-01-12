// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80455

sealed interface <!CLASSIFIER_REDECLARATION!>SE<!>
sealed interface SN : SE
sealed interface SD : SE
sealed interface SND : SD, SN
sealed interface SDP : SE
sealed interface SDC : SDP
sealed interface SMDC : SDP, SDC

sealed class SEB : SE
sealed class SB : SEB(), SE

abstract class SM : SEB(), SMDC, SN

abstract class SC : SB(), SND, SDC

// Mistake: name clash
abstract class <!CLASSIFIER_REDECLARATION!>SE<!> : SEB(), SND, SDC
class SES : SE<!NO_CONSTRUCTOR!>()<!>, SMDC

public val SDP.x: String?
    get() = <!WHEN_ON_SEALED!>when (this) {
        is SM -> null
        is SC -> null
        is SE -> null
    }<!>

/* GENERATED_FIR_TAGS: classDeclaration, getter, interfaceDeclaration, isExpression, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, sealed, whenExpression, whenWithSubject */
