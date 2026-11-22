fun main() {
    Observable.of(1, 2)
        .apply({ observable ->
            Observable { subscriber ->
                (observable).subscribe(object : Observer<Int> {
                    override fun next(value: Int) {
                        subscriber.next(value * 2)
                    }

                })
            }
        })
        .subscribe(object : Subscriber<Int>() {
            override fun next(value: Int) {
            }
        })
}
