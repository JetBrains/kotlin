fun getWrapped2(): Runnable {
  val f = { }
  return Runnable(f)
}
