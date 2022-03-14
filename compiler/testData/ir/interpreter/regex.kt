import kotlin.text.*
import kotlin.collections.*

const val regConstructor1 = <!EVALUATED: `pattern`!>Regex("pattern").pattern<!>
const val regConstructor2 = <!EVALUATED: `IGNORE_CASE`!>Regex("pattern", RegexOption.IGNORE_CASE).options.iterator().next().name<!>
const val regConstructor3 = <!EVALUATED: `2`!>Regex("pattern", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)).options.size<!>

const val matchEntire1 = <!EVALUATED: `pat`!>Regex("pat").matchEntire("pat")?.value.toString()<!>
const val matchEntire2 = <!EVALUATED: `0`!>Regex("[abc]").matchEntire("a")?.range?.last.toString()<!>
const val matches1 = <!EVALUATED: `true`!>Regex("str(1)?").matches("str1")<!>
const val matches2 = <!EVALUATED: `false`!>Regex("str(1)?").matches("str2")<!>
const val containsMatchIn1 = <!EVALUATED: `true`!>Regex("[0-9]").containsMatchIn("0")<!>
const val containsMatchIn2 = <!EVALUATED: `true`!>Regex("[0-9]").containsMatchIn("!!0!!")<!>
const val containsMatchIn3 = <!EVALUATED: `false`!>Regex("[0-9]").containsMatchIn("!!p!!")<!>

//replace
const val replace1 = <!EVALUATED: `There are n apples`!>Regex("0").replace("There are 0 apples", "n")<!>
const val replace2 = <!EVALUATED: `Roses are red!, Violets are blue!`!>Regex("(red|green|blue)").replace("Roses are red, Violets are blue") { it.value + "!" }<!>
const val replace3 = <!EVALUATED: `Roses are REPLACED, Violets are blue`!>Regex("(red|green|blue)").replaceFirst("Roses are red, Violets are blue", "REPLACED")<!>
const val split = <!EVALUATED: `6`!>Regex("\\W+").split("Roses are red, Violets are blue").size<!>

//find
const val find1 = <!EVALUATED: `p`!>Regex("p").find("p")?.value.toString()<!>
const val find2 = <!EVALUATED: `2`!>Regex("(red|green|blue)").find("Roses are red, Violets are blue")?.groups?.size.toString()<!>
const val find3 = <!EVALUATED: `red`!>Regex("(red|green|blue)").find("Roses are red, Violets are blue")?.destructured?.component1().toString()<!>
const val find4 = <!EVALUATED: `blue`!>Regex("(red|green|blue)").find("Roses are red, Violets are blue")?.next()?.value.toString()<!>
const val find5 = <!EVALUATED: `blue`!>Regex("(red|green|blue)").find("Roses are red, Violets are blue", 15)?.value.toString()<!>
const val find6 = <!EVALUATED: `red`!>Regex("(red|green|blue)").findAll("Roses are red, Violets are blue").iterator().next()?.value.toString()<!>
const val find7 = <!EVALUATED: `blue`!>Regex("(red|green|blue)").findAll("Roses are red, Violets are blue").iterator().next()?.next()?.value.toString()<!>
const val find8 = <!EVALUATED: `null`!>Regex("(red|green|blue)").findAll("Roses are red, Violets are blue").iterator().next()?.next()?.next()?.value.toString()<!>

//companion
const val fromLiteral = <!EVALUATED: `[a-z0-9]+`!>Regex.fromLiteral("[a-z0-9]+").pattern<!>
const val escape = <!EVALUATED: `\Q[a-z0-9]+\E`!>Regex.escape("[a-z0-9]+")<!>
const val escapeReplacement = <!EVALUATED: `[a-z0-9]+`!>Regex.escapeReplacement("[a-z0-9]+")<!>
