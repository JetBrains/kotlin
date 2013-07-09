class Super {
    void safeInvoke(Runnable r) {
        if (r != null) r.run();
    }
}

class Sub extends Super {
}
