<info descr="null">abstract</info> class Base {
  <info descr="null">abstract</info> fun foo(<TYPO descr="Typo: In word 'oher'">oher</TYPO>: Int)

  <info descr="null">abstract</info> val <TYPO descr="Typo: In word 'smal'">smal</TYPO>Val: Int
  <info descr="null">abstract</info> fun <TYPO descr="Typo: In word 'smal'">smal</TYPO>Fun()
}

class Other : Base() {
  <info descr="null">override</info> fun foo(oher: Int) {
  }

  <info descr="null">override</info> val smalVal: Int <info descr="null">get</info>() = 1
  <info descr="null">override</info> fun smalFun() {}
}

