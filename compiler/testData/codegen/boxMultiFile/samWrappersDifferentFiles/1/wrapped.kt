fun getWrapped1(): Runnable {
  val f = { }
  return Runnable(f)
}
