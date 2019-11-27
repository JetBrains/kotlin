// IGNORE_BACKEND_FIR: JVM_IR
enum class ClassTemplate(
        // var bug: Int = 1,
        var code: Int,
        var nameTemplate: Int = 1,

        val parent: Int  = 1,
        val previous: Int  = 1,
        val progressionEquivalent: Int  = 1,

        var idDiscipline: Int = 1,
        var strictRunningOrder: Int = 1,
        var pointsMethod: Int = 1,

        var noTimeFaults: Int = 1,
        var combineHeights: Int = 1,

        var column: Int = 1,
        var runningOrderSort: Int = 1,
        var programme: Int = 1,
        var eliminationTime: Int = 1,
        var courseTimeCode: Int = 1,
        var teamSize: Int = 1,
        var sponsor: Int = 1,
        var lateEntryCredits: Int = 1,
        var lateEntryFee: Int = 1,

        var courseLengthNeeded: Int = 1,

        var discretionaryCourseTime: Int = 1,
        var isRelay: Int = 1,
        var isQualifier: Int = 1,
        var generateChildren: Int = 1,
        var feedFromParent: Int = 1,

        var isNfcAllowed: Int = 1,
        var isAddOnAllowed: Int = 1,
        var isSpecialEntry: Int = 1,
        var isUkaProgression: Int = 1,
        var canEnterDirectly: Int = 1,
        var isPointRanked: Int = 1,
        var isPointRankedDesc: Int = 1
) {
    UNDEFINED(code = 56, nameTemplate = 3),
    BLAH(code = 57, nameTemplate = 4)
}

fun box(): String {
    val x = ClassTemplate.UNDEFINED
    val y = ClassTemplate.BLAH

    if (x.code != 56 || x.nameTemplate != 3 || x.isAddOnAllowed != 1) return "fail 1"
    if (y.code != 57 || y.nameTemplate != 4 || y.isAddOnAllowed != 1) return "fail 2"

    return "OK"
}
