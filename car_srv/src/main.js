/**
 * Created by user on 7/11/16.
 */

const server = require("./server.js");
const router = require("./router.js");
const commander = require("commander");
const protoBuf = require("protobufjs");
const fs = require("fs");

commander
    .version('1.0.0')
    .option('-p, --proto [path]', 'path to dir with proto files. need here carkot.proto and route.proto. default ./proto/')
    .option('-f, --flash [path]', 'path to save bin file with name f.bin. default ./')
    .option('-s, --serial [path]', 'path to file that represents MCU serial port default ./serial')
    .parse(process.argv);

//add slash to end of paths if need
if (commander.protopath) {
    if (commander.protopath.substr(commander.protopath.length - 1) != "/") {
        commander.protopath = commander.protopath + "/";
    }
}
if (commander.flash) {
    if (commander.flash.substr(commander.flash.length - 1) != "/") {
        commander.flash = commander.flash + "/";
    }
}

const builderCarkot = protoBuf.loadProtoFile(commander.protopath ? commander.protopath + "carkot.proto" : "./proto/carkot.proto");//todo!!1 путь до каталога, прото файлы - в нём
const builderControl = protoBuf.loadProtoFile(commander.protopath ? commander.protopath + "route.proto" : "./proto/route.proto");

var executeShell = "./st-flash";
exports.protoConstructorCarkot = builderCarkot.build("carkot");
exports.protoConstructorControl = builderControl.build("carkot");
exports.commandPrefix = executeShell + " write";
exports.binFilePath = (commander.flash ? commander.flash : "./") + "st-flash";
exports.transportFilePath = (commander.serial ? commander.serial : "./serial");

var handlers = require("./handlers.js");
var handle = {};
handle["/"] = handlers.other;
handle["/loadBin"] = handlers.loadBin;
handle["/control"] = handlers.control;
handle["/other"] = handlers.other;

if (typeof fs.access == "function") {
    fs.access(executeShell, fs.F_OK, function (error) {
        if (!error) {
            server.start(router.route, handle);
        } else {
            console.log("file " + executeShell + " not found. Copy this file to server root dir");
        }
    });
} else {
    console.log("warning: you have old version of node.js. Check existence of the file st-flash on root of server dir yourself");
    server.start(router.route, handle);
}
// server.start(router.route, handle);
