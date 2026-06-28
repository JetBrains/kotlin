// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
private const val DEFAULT_SETTINGS_STRING = "800_2000_200_400"

<!POSSIBLY_UNINITIALIZED_PROPERTY!>private val DEFAULT_SETTINGS = <!ACCESSING_POSSIBLY_UNINITIALIZED_PROPERTY!>parseSettings(DEFAULT_SETTINGS_STRING)<!><!>

private data class Settings(
  val disableMinMillis: Long,
  val disableMaxMillis: Long,
  val enableMinMillis: Long,
  val enableMaxMillis: Long
)

private fun parseSettings(settingsString: String): Settings {
  val numberStrings = settingsString.split("_")
  if (numberStrings.size != 4) {
    return DEFAULT_SETTINGS
  }

  val numbers = try {
    numberStrings.map { it.toLong() }
  }
  catch (_: NumberFormatException) {
    return DEFAULT_SETTINGS
  }

  val disableMinMillis = numbers[0]
  val disableMaxMillis = numbers[1]
  val enableMinMillis = numbers[2]
  val enableMaxMillis = numbers[3]

  if ((disableMinMillis in 0 until disableMaxMillis) && (enableMinMillis in 0 until enableMaxMillis)) {
    return Settings(
      disableMinMillis,
      disableMaxMillis,
      enableMinMillis,
      enableMaxMillis
    )
  }

  return DEFAULT_SETTINGS
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, const, data, equalityExpression, functionDeclaration,
ifExpression, integerLiteral, lambdaLiteral, localProperty, primaryConstructor, propertyDeclaration, stringLiteral,
tryExpression, unnamedLocalVariable */
