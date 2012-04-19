val language = if (args.size == 0) "EN" else args[0]
when (language) {
 "EN" -> "Hello!"
 "ES" -> "¡Hola!"
 "RU" -> "Привет!"
 else -> "Sorry, I can't greet you in $language yet"
}