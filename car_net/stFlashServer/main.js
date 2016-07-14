/**
 * Created by user on 7/11/16.
 */

var server = require("./server.js");
var router = require("./router.js");
var commander = require("commander");
const protoBuf = require("protobufjs");

commander
    .version('1.0.0')
    .option('-p, --protopath [path]', 'path to dir with proto files. need here carkot.proto and route.proto. default ./proto/')
    .option('-b, --binfile [path]', 'path to save bin file with name f.bin. default ./')
    .option('-t, --transportfile [path]', 'path to trasport file for connect to mcu default ./mcu')
    .parse(process.argv);

//add slash to end of paths if need
if (commander.protopath) {
    if (commander.protopath.substr(commander.protopath.length - 1) != "/") {
        commander.protopath = commander.protopath + "/";
    }
}
if (commander.binfile) {
    if (commander.binfile.substr(commander.binfile.length - 1) != "/") {
        commander.binfile = commander.binfile + "/";
    }
}

const builderCarkot = protoBuf.loadProtoFile(commander.protopath ? commander.protopath + "carkot.proto" : "./proto/carkot.proto");//todo!!1 путь до каталога, прото файлы - в нём
const builderControl = protoBuf.loadProtoFile(commander.protopath ? commander.protopath + "route.proto" : "./proto/route.proto");

exports.protoConstructorCarkot = builderCarkot.build("carkot");
exports.protoConstructorControl = builderControl.build("carkot");
exports.commandPrefix = "./st-flash write";
exports.binFilePath = (commander.binfile ? commander.binfile : "./") + "f.bin";
exports.transportFilePath = (commander.transportfile ? commander.transportfile : "./mcu");

var handlers = require("./handlers.js");
var handle = {};
handle["/"] = handlers.other;
handle["/loadBin"] = handlers.loadBin;
handle["/control"] = handlers.control;
handle["/other"] = handlers.other;

server.start(router.route, handle);