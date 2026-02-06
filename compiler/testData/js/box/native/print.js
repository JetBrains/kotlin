var buffer = "";

function writeToBuffer(a) {
    var type = typeof a;
    if (type === "undefined") return;
    if (type !== "string" && !(a instanceof String)) throw Error("Expected string argument type, but got: " + type);

    buffer += a;
}

function writelnToBuffer(a) {
    writeToBuffer(a);
    writeToBuffer("\n");
}

var GLOBAL = (0, eval)("this");

GLOBAL.console = {
    log: function (a) {
        if (typeof a !== "undefined") {
            buffer += a
        }
        buffer += "\n";
    }
};

GLOBAL.outputStream =  {
    write: writeToBuffer
};

