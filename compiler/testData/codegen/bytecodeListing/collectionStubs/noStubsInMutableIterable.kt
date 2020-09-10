class MyIterable<E> : MutableIterable<E> {
    class MyIterator<E> : MutableIterator<E> {
        override fun hasNext(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun next(): E {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun remove() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    override fun iterator(): MutableIterator<E> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}