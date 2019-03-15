fun main(args: Array<String>) {
  listO<caret>f(20, 30).map { if(it == 20) null else it }.count()
}