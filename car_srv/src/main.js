/**
 * Created by maxim zaitsev on 7/11/16.
 */

const server = require("./server.js");
const router = require("./router.js");
const commander = require("commander");
const protoBuf = require("protobufjs");
const fs = require("fs");
const udev = require("udev");
const client = require("./client.js");

//parsing command line args
commander
    .version('1.0.0')
    .option('-p, --proto [path]', 'path to dir with proto files. need here carkot.proto and route.proto. default ./proto/')
    .option('-f, --flash [path]', 'path to save bin file with name flash.bin. default ./')
    // .option('-s, --serial [path]', 'path to file that represents MCU serial port default ./serial')//dont need more. use path from udev monitor
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

//classes for protobuf decode/encode
const builderCarkot = protoBuf.loadProtoFile((commander.protopath ? commander.protopath : "./proto/") + "carkot.proto");
const builderControl = protoBuf.loadProtoFile((commander.protopath ? commander.protopath : "./proto/" + "direction.proto"));
const builderRoute = protoBuf.loadProtoFile((commander.protopath ? commander.protopath : "./proto/" + "route.proto"));
const builderConnect = protoBuf.loadProtoFile((commander.protopath ? commander.protopath : "./proto/" + "connect.proto"));

var executeShell = "./st-flash";
exports.protoConstructorCarkot = builderCarkot.build("carkot");
exports.protoConstructorControl = builderControl.build("carkot");
exports.protoConstructorRoute = builderRoute.build("carkot");
exports.protoConstructorConnect = builderConnect.build("carkot");
exports.commandPrefix = executeShell + " write";
exports.binFilePath = (commander.flash ? commander.flash : "./") + "flash.bin";
exports.transportFilePath = "/dev/ttyACM0";//todo плохо:)
exports.serverAddress = "127.0.0.1";
exports.serverPort = 7925;

//init this car
var connectRequest = new exports.protoConstructorConnect.ConnectionRequest({
    // "ip": "192.168.43.135",//todo its ip of this server in local wifi network. hardcore is bad here:)
    "ip": "127.0.0.1",//todo for tests on local mashine
    "port": 8888
});
client.sendData(connectRequest.encode().buffer, "/connect", function (data) {
    var connectionResponse = exports.protoConstructorConnect.ConnectionResponse.decode(data);
    exports.thisCar.uid = connectionResponse.uid
});
var car = require("./car.js").getCar();
exports.thisCar = car;

//handlers for client requests
var handle = {};
handle["/loadBin"] = require("./handlers/loadBinHandler").handler;
handle["/control"] = require("./handlers/controlHandler").handler;
handle["/route"] = require("./handlers/setRouteHandler").handler;

//add event handlers from udev monitor (add device and remove device)
const monitor = udev.monitor();
monitor.on('add', function (device) {
    if (device.ID_VENDOR_ID == "0483" && device.ID_MODEL_ID == "5740" && device.SUBSYSTEM == "tty") {
        //mc connected
        console.log("connected. transport file is " + device.DEVNAME);
        exports.transportFilePath = device.DEVNAME;
        console.log(device.DEVNAME);
    }
});
monitor.on('remove', function (device) {
    if (device.ID_VENDOR_ID == "0483" && device.ID_MODEL_ID == "5740" && device.SUBSYSTEM == "tty") {
        //mc disconnected
        console.log("disconnected")
    }
});

//check of exists st-flash util
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

//move car
var delta = 100;
setInterval(moveCar, delta);
function moveCar() {
    car.move(delta)
}