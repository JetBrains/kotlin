open class Developer
class Person(val name: String) : Developer()
class Company(val name: String, val cto: Person) : Developer()

sealed class Download
data class App(val title: String, val developer: Developer) : Download()
data class Movie(val title: String, val director: Person) : Download()

fun f(download: Download) = when {
  download is App
    && val (title, developer) = download
    && developer is Person
    && developer.name == "Alice" -> "$title in Wonderland"
  else -> "Boo"
}

fun g(download: Download) = when {
  download is App (title, developer)
    && developer is Person
    && developer.name == "Alice" -> "$title in Wonderland"
  else -> "Boo"
}
