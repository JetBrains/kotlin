from SimpleXMLRPCServer import SimpleXMLRPCServer
import os
import tempfile

def getReq():
    req = {
        'flash_address': '0x08000000',
        'data': '',
        'device': '',
        'method': 'STLINK'
    }
    data = os.path.join(os.getcwd(), 'devloader', 'data', 'fancyblink.bin')
    with open(data, 'rb') as f:
        req['data'] = f.read()
        f.close()
    print 'Image has', len(req['data']), 'bytes'
    return req


def getFlasher(method, data, address):
    flasher = {}
    os_arch = 'macosx-x86_64'
    root = os.path.join(os.getcwd(), 'devloader', 'data')
    if method == 'STLINK':
        flasher['program'] = os.path.join(root, os_arch, 'st-flash')
        flasher['args'] = ['write', data, address]
    else:
        return None
    return flasher


def processReq(req):
    temp = tempfile.mktemp()
    result = { 'output': '' }
    with open(temp, 'wb') as f:
        f.write(req['data'])
        f.close()
        flasher = getFlasher(req['method'], temp, req['flash_address'])
        cmd = ' '.join([flasher['program']] + flasher['args'] + ["2>&1"])
        with os.popen(cmd, 'r') as pipe:
            for line in pipe:
                result['output'] = result['output'] + line
    print 'res=', result['output']
    os.unlink(temp)
    return result



class Handler:
    def __init__(self):
        # make all of the string functions available through
        # string.func_name
        import string
        self.string = string
        
    def _listMethods(self):
        # implement this method so that system.listMethods
        # knows to advertise the strings methods
        return list_public_methods(self) + \
                ['string.' + method for method in list_public_methods(self.string)]
        
    def flash(self, req):
        return processReq(req)

server = SimpleXMLRPCServer(("localhost", 8000))
server.register_introspection_functions()
server.register_instance(Handler())
server.serve_forever()

def main0():
    req = getReq()
    processReq(req)


def main():
    server = SimpleXMLRPCServer(("localhost", 8000))
    server.register_introspection_functions()
    server.register_instance(Handler())
    server.serve_forever()
    
if __name__ == "__main__":
    main()
