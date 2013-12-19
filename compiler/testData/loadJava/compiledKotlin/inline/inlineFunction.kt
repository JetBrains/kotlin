package test

inline fun a() {}

inline(InlineStrategy.AS_FUNCTION) fun b() {}

inline(InlineStrategy.IN_PLACE) fun c() {}

inline(strategy = InlineStrategy.IN_PLACE) fun d() {}