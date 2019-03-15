fun main(args: Array<String>) {
  "jetBrains".map { it.isLowerCase() }
      .flat<caret>Map { linkedSetOf(1.2, 3.0) }
      .map { it.toString() }
      .count()
}