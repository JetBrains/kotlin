/**
 * Created by user on 7/11/16.
 */

var server = require("./server.js");
var router = require("./router.js");
var handlers = require("./handlers.js");
var commander = require("commander");
const protoBuf = require("protobufjs");


commander
    .version('1.0.0')
    .option('-p, --protopath [path]', 'path to proto file. default ./proto/carkot.proto')
    .option('-b, --binfile [path]', 'path to save bin file with name f.bin. default ./')
    // .option('-b, --bbq-sauce', 'Add bbq sauce')
    // .option('-c, --cheese [type]', 'Add the specified type of cheese [marble]', 'marble')
    .parse(process.argv);

const builder = protoBuf.loadProtoFile(commander.protopath ? commander.protopath : "./proto/carkot.proto");
const protoConstructor = builder.build("carkot");

if (commander.protopath) {
    console.log("pew " + commander.protopath)
}
var handle = {};
handle["/"] = handlers.other;
handle["/loadBin"] = handlers.loadBin;
handle["/other"] = handlers.other;

server.start(router.route, handle);
exports.protoConstructor = protoConstructor;
exports.commandPrefix = "./st-flash write";
exports.binFilePath = (commander.binfile ? commander.binfile : "./") + "f.bin";
console.log(exports.binFilePath);
console.log(exports.commandPrefix);