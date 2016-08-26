val x = listOf(1).map { q -> println(<caret>q) }

// TYPE: q -> <html>Int</html>
// TYPE: println(q) -> <html>Unit</html>
// TYPE: { q -> println(q) } -> <html>(Int) &rarr; Unit</html>
// TYPE: listOf(1).map { q -> println(q) } -> <html>List&lt;Unit&gt;</html>
// TYPE: val x = listOf(1).map { q -> println(q) } -> <html>List&lt;Unit&gt;</html>
