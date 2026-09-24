value class AbstractToMfvcSubclass(val first: Int, val second: Int) : AbstractToMfvc(first, second)

fun mfvcToInline() = MfvcToInline(1).first
fun inlineToMfvc() = InlineToMfvc(1).first

fun oneToTwoParameters() = OneToTwoParams(1).first
fun twoToOneParameters1() = TwoToOneParams(1, 2).first
fun twoToOneParameters2() = TwoToOneParams(1, 2).second

fun mfvcToAbstract() = MfvcToAbstract(1, 2).first
fun abstractToMfvc() = AbstractToMfvcSubclass(1, 2).first

fun classToMfvc1() = ClassToMfvc(1, 2).first
fun classToMfvc2() = ClassToMfvc(1, 2).second
fun mfvcToClass1() = MfvcToClass(1, 2).first
fun mfvcToClass2() = MfvcToClass(1, 2).second

fun inlineToMfvcVersionOverload() = InlineToMfvcVersionOverload(1).first
fun mfvcVersionOverload() = MfvcVersionOverload(1).first
