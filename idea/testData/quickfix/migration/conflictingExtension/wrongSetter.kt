// "Delete redundant extension property" "false"

var Thread.<caret>priority: Int
    get() = this.getPriority()
    set(value) {
        this.setPriority(value)
        System.out.print("set")
    }
