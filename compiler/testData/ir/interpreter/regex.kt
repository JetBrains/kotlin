import kotlin.text.*
import kotlin.collections.*

const val regConstructor1 = Regex("pattern").<!EVALUATED: `pattern`!>pattern<!>
const val regConstructor2 = Regex("pattern", RegexOption.IGNORE_CASE).options.iterator().next().<!EVALUATED: `IGNORE_CASE`!>name<!>
const val regConstructor3 = Regex("pattern", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)).options.<!EVALUATED: `2`!>size<!>

const val matchEntire1 = Regex("pat").matchEntire("pat")?.value.<!EVALUATED: `pat`!>toString()<!>
const val matchEntire2 = Regex("[abc]").matchEntire("a")?.range?.last.<!EVALUATED: `0`!>toString()<!>
const val matches1 = Regex("str(1)?").<!EVALUATED: `true`!>matches("str1")<!>
const val matches2 = Regex("str(1)?").<!EVALUATED: `false`!>matches("str2")<!>
const val containsMatchIn1 = Regex("[0-9]").<!EVALUATED: `true`!>containsMatchIn("0")<!>
const val containsMatchIn2 = Regex("[0-9]").<!EVALUATED: `true`!>containsMatchIn("!!0!!")<!>
const val containsMatchIn3 = Regex("[0-9]").<!EVALUATED: `false`!>containsMatchIn("!!p!!")<!>

//replace
const val replace1 = Regex("0").<!EVALUATED: `There are n apples`!>replace("There are 0 apples", "n")<!>
const val replace2 = Regex("(red|green|blue)").<!EVALUATED: `Roses are red!, Violets are blue!`!>replace("Roses are red, Violets are blue") { it.value + "!" }<!>
const val replace3 = Regex("(red|green|blue)").<!EVALUATED: `Roses are REPLACED, Violets are blue`!>replaceFirst("Roses are red, Violets are blue", "REPLACED")<!>
const val split = Regex("\\W+").split("Roses are red, Violets are blue").<!EVALUATED: `6`!>size<!>

//find
const val find1 = Regex("p").find("p")?.value.<!EVALUATED: `p`!>toString()<!>
const val find2 = Regex("(red|green|blue)").find("Roses are red, Violets are blue")?.groups?.size.<!EVALUATED: `2`!>toString()<!>
const val find3 = Regex("(red|green|blue)").find("Roses are red, Violets are blue")?.destructured?.component1().<!EVALUATED: `red`!>toString()<!>
const val find4 = Regex("(red|green|blue)").find("Roses are red, Violets are blue")?.next()?.value.<!EVALUATED: `blue`!>toString()<!>
const val find5 = Regex("(red|green|blue)").find("Roses are red, Violets are blue", 15)?.value.<!EVALUATED: `blue`!>toString()<!>
const val find6 = Regex("(red|green|blue)").findAll("Roses are red, Violets are blue").iterator().next()?.value.<!EVALUATED: `red`!>toString()<!>
const val find7 = Regex("(red|green|blue)").findAll("Roses are red, Violets are blue").iterator().next()?.next()?.value.<!EVALUATED: `blue`!>toString()<!>
const val find8 = Regex("(red|green|blue)").findAll("Roses are red, Violets are blue").iterator().next()?.next()?.next()?.value.<!EVALUATED: `null`!>toString()<!>

//companion
const val fromLiteral = Regex.fromLiteral("[a-z0-9]+").<!EVALUATED: `[a-z0-9]+`!>pattern<!>
const val escape = Regex.<!EVALUATED: `\Q[a-z0-9]+\E`!>escape("[a-z0-9]+")<!>
const val escapeReplacement = Regex.<!EVALUATED: `[a-z0-9]+`!>escapeReplacement("[a-z0-9]+")<!>
