var log = "";

function C() {
}
Object.defineProperty(C.prototype, "foo", {
    "get" : function () {
        log += "foo called";
        return "OK"
    }
});